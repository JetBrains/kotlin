/*
 * Reworker but not completely refactored lib from
 * http://microtechnics.ru/biblioteka-dlya-raboty-s-usart-v-stm32/
 */
#include "usart.h"
#include <stdbool.h>
#include <stm32f4xx_rcc.h>
#include <stm32f4xx_usart.h>

// Variables for GPIO and USART configuration
GPIO_InitTypeDef MT_USART_CfgGPIO;
USART_InitTypeDef MT_USART_CfgUSART;

typedef struct {
    USART_TypeDef *usart;
    // Ready to transmit and ready to receive flags
    volatile bool MT_USART_ReadyToExchange;
    // Number of bytes to send
    uint8_t MT_USART_NumOfDataToSend;
    // Number of bytes to receive
    uint8_t MT_USART_NumOfDataToReceive;
    // Pointer to the data that will be sent
    uint8_t *MT_USART_SendDataPtr;
    // Pointer to the buffer where received data will be saved
    uint8_t *MT_USART_ReceivedDataPtr;
    // Number of sended bytes
    volatile uint8_t MT_USART_NumOfDataSended;
    // Number of received bytes
    volatile uint8_t MT_USART_NumOfDataReceived;
} usart_state_t;

static usart_state_t usart_states[3] = {
    {
        .usart = USART1,
        .MT_USART_ReadyToExchange = true
    },
    {
        .usart = USART2,
        .MT_USART_ReadyToExchange = true
    },
    {
        .usart = USART3,
        .MT_USART_ReadyToExchange = true
    }
};

static void usart1_init(void)
{
    RCC_APB2PeriphClockCmd(RCC_APB2Periph_USART1, ENABLE);
	RCC_AHB1PeriphClockCmd(RCC_AHB1Periph_GPIOA, ENABLE);
	
	GPIO_StructInit(&MT_USART_CfgGPIO);
	MT_USART_CfgGPIO.GPIO_Mode = GPIO_Mode_AF;
	MT_USART_CfgGPIO.GPIO_Pin = GPIO_Pin_9;
	MT_USART_CfgGPIO.GPIO_Speed = GPIO_Speed_50MHz;
	MT_USART_CfgGPIO.GPIO_OType = GPIO_OType_PP;
	MT_USART_CfgGPIO.GPIO_PuPd = GPIO_PuPd_UP;
 	GPIO_Init(GPIOA, &MT_USART_CfgGPIO);
	
	MT_USART_CfgGPIO.GPIO_Mode = GPIO_Mode_AF;
	MT_USART_CfgGPIO.GPIO_Pin = GPIO_Pin_10;
	MT_USART_CfgGPIO.GPIO_Speed = GPIO_Speed_50MHz;
	MT_USART_CfgGPIO.GPIO_OType = GPIO_OType_PP;
	MT_USART_CfgGPIO.GPIO_PuPd = GPIO_PuPd_UP;
    GPIO_Init(GPIOA, &MT_USART_CfgGPIO);
	
	GPIO_PinAFConfig(GPIOA, GPIO_PinSource9, GPIO_AF_USART1);
	GPIO_PinAFConfig(GPIOA, GPIO_PinSource10, GPIO_AF_USART1);
	
	USART_StructInit(&MT_USART_CfgUSART);
	MT_USART_CfgUSART.USART_Mode = USART_Mode_Rx | USART_Mode_Tx;
	MT_USART_CfgUSART.USART_BaudRate = MT_USART_BAUDRATE;
	USART_Init(USART1, &MT_USART_CfgUSART);	
	
	//NVIC_EnableIRQ(USART1_IRQn);
	USART_Cmd(USART1, ENABLE);
}

static void usart2_init(void)
{
    RCC_APB1PeriphClockCmd(RCC_APB1Periph_USART2, ENABLE);
	RCC_AHB1PeriphClockCmd(RCC_AHB1Periph_GPIOA, ENABLE);
	
	GPIO_StructInit(&MT_USART_CfgGPIO);
	MT_USART_CfgGPIO.GPIO_Mode = GPIO_Mode_AF;
	MT_USART_CfgGPIO.GPIO_Pin = GPIO_Pin_2;
	MT_USART_CfgGPIO.GPIO_Speed = GPIO_Speed_50MHz;
	MT_USART_CfgGPIO.GPIO_OType = GPIO_OType_PP;
	MT_USART_CfgGPIO.GPIO_PuPd = GPIO_PuPd_UP;
 	GPIO_Init(GPIOA, &MT_USART_CfgGPIO);
	
	GPIO_StructInit(&MT_USART_CfgGPIO);
	MT_USART_CfgGPIO.GPIO_Mode = GPIO_Mode_AF;
	MT_USART_CfgGPIO.GPIO_Pin = GPIO_Pin_3;
	MT_USART_CfgGPIO.GPIO_Speed = GPIO_Speed_50MHz;
	MT_USART_CfgGPIO.GPIO_OType = GPIO_OType_PP;
	MT_USART_CfgGPIO.GPIO_PuPd = GPIO_PuPd_UP;
    GPIO_Init(GPIOA, &MT_USART_CfgGPIO);
	
	GPIO_PinAFConfig(GPIOA, GPIO_PinSource2, GPIO_AF_USART2);
	GPIO_PinAFConfig(GPIOA, GPIO_PinSource3, GPIO_AF_USART2);
	
	USART_StructInit(&MT_USART_CfgUSART);
	MT_USART_CfgUSART.USART_Mode = USART_Mode_Rx | USART_Mode_Tx;
	MT_USART_CfgUSART.USART_BaudRate = MT_USART_BAUDRATE;
	USART_Init(USART2, &MT_USART_CfgUSART);	

	//NVIC_EnableIRQ(USART2_IRQn);
	USART_Cmd(USART2, ENABLE);

}

static void usart3_init(void)
{
    RCC_APB1PeriphClockCmd(RCC_APB1Periph_USART3, ENABLE);
	RCC_AHB1PeriphClockCmd(RCC_AHB1Periph_GPIOC, ENABLE);
	
	GPIO_StructInit(&MT_USART_CfgGPIO);
	MT_USART_CfgGPIO.GPIO_Mode = GPIO_Mode_AF;
	MT_USART_CfgGPIO.GPIO_Pin = GPIO_Pin_10;
	MT_USART_CfgGPIO.GPIO_Speed = GPIO_Speed_50MHz;
	MT_USART_CfgGPIO.GPIO_OType = GPIO_OType_PP;
	MT_USART_CfgGPIO.GPIO_PuPd = GPIO_PuPd_UP;
 	GPIO_Init(GPIOC, &MT_USART_CfgGPIO);
	
	MT_USART_CfgGPIO.GPIO_Mode = GPIO_Mode_AF;
	MT_USART_CfgGPIO.GPIO_Pin = GPIO_Pin_11;
	MT_USART_CfgGPIO.GPIO_Speed = GPIO_Speed_50MHz;
	MT_USART_CfgGPIO.GPIO_OType = GPIO_OType_PP;
	MT_USART_CfgGPIO.GPIO_PuPd = GPIO_PuPd_UP;
    GPIO_Init(GPIOC, &MT_USART_CfgGPIO);
	
	GPIO_PinAFConfig(GPIOC, GPIO_PinSource10, GPIO_AF_USART3);
	GPIO_PinAFConfig(GPIOC, GPIO_PinSource11, GPIO_AF_USART3);
	
	USART_StructInit(&MT_USART_CfgUSART);
	MT_USART_CfgUSART.USART_Mode = USART_Mode_Rx | USART_Mode_Tx;
	MT_USART_CfgUSART.USART_BaudRate = MT_USART_BAUDRATE;
	USART_Init(USART3, &MT_USART_CfgUSART);	
	
	//NVIC_EnableIRQ(USART3_IRQn);
	USART_Cmd(USART3, ENABLE);
}

void usart_init(enum usart_id_t usart_id)
{
    switch(usart_id) {
        case USART1_ID:
            usart1_init();
            break;
        case USART2_ID:
            usart2_init();
            break;
        case USART3_ID:
            usart3_init();
            break;
    }
}

void usart_send_data(enum usart_id_t usart_id, uint8_t *data, size_t size)
{
    usart_state_t *usart = &usart_states[usart_id];
    while (!usart->MT_USART_ReadyToExchange);

    usart->MT_USART_SendDataPtr = data;
	usart->MT_USART_NumOfDataToSend = size;
	usart->MT_USART_NumOfDataSended = 0;
	usart->MT_USART_ReadyToExchange = false;
	//USART_ITConfig(usart->usart, USART_IT_TC, ENABLE);

    while (usart->MT_USART_NumOfDataSended != size) {
		USART_SendData(usart->usart, (uint8_t)*usart->MT_USART_SendDataPtr);
        while (USART_GetFlagStatus(usart->usart, USART_FLAG_TC) == RESET);
		usart->MT_USART_SendDataPtr++;
		usart->MT_USART_NumOfDataSended++;
    }
	usart->MT_USART_ReadyToExchange = true;
}

void usart_rcv_data(enum usart_id_t usart_id, uint8_t *data, size_t size)
{
    usart_state_t *usart = &usart_states[usart_id];
    while (!usart->MT_USART_ReadyToExchange);

	usart->MT_USART_ReceivedDataPtr = data;
	usart->MT_USART_NumOfDataToReceive = size;
	usart->MT_USART_NumOfDataReceived = 0;
	usart->MT_USART_ReadyToExchange = false;
	//USART_ITConfig(usart->usart, USART_IT_RXNE, ENABLE);

    while (usart->MT_USART_NumOfDataReceived != size) {
        while (USART_GetFlagStatus(usart->usart, USART_FLAG_RXNE) == RESET);
		*usart->MT_USART_ReceivedDataPtr = USART_ReceiveData(usart->usart);	
		usart->MT_USART_ReceivedDataPtr++;
		usart->MT_USART_NumOfDataReceived++;
    }
    usart->MT_USART_ReadyToExchange = true;
}

void USART_IRQHandler(enum usart_id_t usart_id)
{
    usart_state_t *usart = &usart_states[usart_id];

    if (USART_GetITStatus(usart->usart, USART_IT_RXNE) != RESET)
	{	
		USART_ClearITPendingBit(usart->usart, USART_IT_RXNE);
		*usart->MT_USART_ReceivedDataPtr = USART_ReceiveData(usart->usart);	
		usart->MT_USART_ReceivedDataPtr++;
		usart->MT_USART_NumOfDataReceived++;
		if (usart->MT_USART_NumOfDataReceived == usart->MT_USART_NumOfDataToReceive)
		{
			USART_ITConfig(usart->usart, USART_IT_RXNE, DISABLE);
			usart->MT_USART_ReadyToExchange = true;
		}
	}
	
	if (USART_GetITStatus(usart->usart, USART_IT_TC) != RESET)
	{
		USART_ClearITPendingBit(usart->usart, USART_IT_TC);
		USART_SendData(usart->usart, (uint8_t)*usart->MT_USART_SendDataPtr);	
		usart->MT_USART_SendDataPtr++;
		usart->MT_USART_NumOfDataSended++;
		if (usart->MT_USART_NumOfDataSended == usart->MT_USART_NumOfDataToSend)
		{
			USART_ITConfig(usart->usart, USART_IT_TC, DISABLE);
			usart->MT_USART_ReadyToExchange = true;
		}		
	}
}
