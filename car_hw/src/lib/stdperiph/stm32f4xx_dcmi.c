/**
  ******************************************************************************
  * @file    stm32f4xx_dcmi.c
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file provides firmware functions to manage the following 
  *          functionalities of the DCMI peripheral:           
  *           - Initialization and Configuration
  *           - Image capture functions  
  *           - Interrupts and flags management
  *
  *  @verbatim  
  *  
  *        
  *          ===================================================================
  *                                 How to use this driver
  *          ===================================================================  
  *         
  *         The sequence below describes how to use this driver to capture image
  *         from a camera module connected to the DCMI Interface.
  *         This sequence does not take into account the configuration of the  
  *         camera module, which should be made before to configure and enable
  *         the DCMI to capture images.
  *           
  *          1. Enable the clock for the DCMI and associated GPIOs using the following functions:
  *                 RCC_AHB2PeriphClockCmd(RCC_AHB2Periph_DCMI, ENABLE);
  *                 RCC_AHB1PeriphClockCmd(RCC_AHB1Periph_GPIOx, ENABLE);
  *
  *          2. DCMI pins configuration 
  *             - Connect the involved DCMI pins to AF13 using the following function 
  *                 GPIO_PinAFConfig(GPIOx, GPIO_PinSourcex, GPIO_AF_DCMI); 
  *             - Configure these DCMI pins in alternate function mode by calling the function
  *                 GPIO_Init();
  *    
  *          3. Declare a DCMI_InitTypeDef structure, for example:
  *                 DCMI_InitTypeDef  DCMI_InitStructure;
  *             and fill the DCMI_InitStructure variable with the allowed values
  *             of the structure member.
  *  
  *          4. Initialize the DCMI interface by calling the function
  *                 DCMI_Init(&DCMI_InitStructure); 
  *  
  *          5. Configure the DMA2_Stream1 channel1 to transfer Data from DCMI DR
  *             register to the destination memory buffer.
  *  
  *          6. Enable DCMI interface using the function
  *                 DCMI_Cmd(ENABLE);
  *                 
  *         7. Start the image capture using the function
  *                 DCMI_CaptureCmd(ENABLE);
  *                 
  *         8. At this stage the DCMI interface waits for the first start of frame,
  *            then a DMA request is generated continuously/once (depending on the
  *            mode used, Continuous/Snapshot) to transfer the received data into
  *            the destination memory. 
  *   
  *  @note  If you need to capture only a rectangular window from the received
  *         image, you have to use the DCMI_CROPConfig() function to configure 
  *         the coordinates and size of the window to be captured, then enable 
  *         the Crop feature using DCMI_CROPCmd(ENABLE);  
  *         In this case, the Crop configuration should be made before to enable
  *         and start the DCMI interface. 
  *        
  *  @endverbatim   
  *  
  ******************************************************************************
  * @attention
  *
  * THE PRESENT FIRMWARE WHICH IS FOR GUIDANCE ONLY AIMS AT PROVIDING CUSTOMERS
  * WITH CODING INFORMATION REGARDING THEIR PRODUCTS IN ORDER FOR THEM TO SAVE
  * TIME. AS A RESULT, STMICROELECTRONICS SHALL NOT BE HELD LIABLE FOR ANY
  * DIRECT, INDIRECT OR CONSEQUENTIAL DAMAGES WITH RESPECT TO ANY CLAIMS ARISING
  * FROM THE CONTENT OF SUCH FIRMWARE AND/OR THE USE MADE BY CUSTOMERS OF THE
  * CODING INFORMATION CONTAINED HEREIN IN CONNECTION WITH THEIR PRODUCTS.
  *
  * <h2><center>&copy; COPYRIGHT 2011 STMicroelectronics</center></h2>
  ******************************************************************************
  */

/* Includes ------------------------------------------------------------------*/
#include "stm32f4xx_conf.h"
#include "stm32f4xx_dcmi.h"
#include "stm32f4xx_rcc.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @defgroup DCMI 
  * @brief DCMI driver modules
  * @{
  */ 

/* Private typedef -----------------------------------------------------------*/
/* Private define ------------------------------------------------------------*/
/* Private macro -------------------------------------------------------------*/
/* Private variables ---------------------------------------------------------*/
/* Private function prototypes -----------------------------------------------*/
/* Private functions ---------------------------------------------------------*/

/** @defgroup DCMI_Private_Functions
  * @{
  */ 

/** @defgroup DCMI_Group1 Initialization and Configuration functions
 *  @brief   Initialization and Configuration functions 
 *
@verbatim   
 ===============================================================================
                  Initialization and Configuration functions
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Deinitializes the DCMI registers to their default reset values.
  * @param  None
  * @retval None
  */
void DCMI_DeInit(void)
{
  DCMI->CR = 0x0;
  DCMI->IER = 0x0;
  DCMI->ICR = 0x1F;
  DCMI->ESCR = 0x0;
  DCMI->ESUR = 0x0;
  DCMI->CWSTRTR = 0x0;
  DCMI->CWSIZER = 0x0;
}

/**
  * @brief  Initializes the DCMI according to the specified parameters in the DCMI_InitStruct.
  * @param  DCMI_InitStruct: pointer to a DCMI_InitTypeDef structure that contains 
  *         the configuration information for the DCMI.
  * @retval None
  */
void DCMI_Init(DCMI_InitTypeDef* DCMI_InitStruct)
{
  uint32_t temp = 0x0;
  
  /* Check the parameters */
  assert_param(IS_DCMI_CAPTURE_MODE(DCMI_InitStruct->DCMI_CaptureMode));
  assert_param(IS_DCMI_SYNCHRO(DCMI_InitStruct->DCMI_SynchroMode));
  assert_param(IS_DCMI_PCKPOLARITY(DCMI_InitStruct->DCMI_PCKPolarity));
  assert_param(IS_DCMI_VSPOLARITY(DCMI_InitStruct->DCMI_VSPolarity));
  assert_param(IS_DCMI_HSPOLARITY(DCMI_InitStruct->DCMI_HSPolarity));
  assert_param(IS_DCMI_CAPTURE_RATE(DCMI_InitStruct->DCMI_CaptureRate));
  assert_param(IS_DCMI_EXTENDED_DATA(DCMI_InitStruct->DCMI_ExtendedDataMode));

  /* The DCMI configuration registers should be programmed correctly before 
  enabling the CR_ENABLE Bit and the CR_CAPTURE Bit */
  DCMI->CR &= ~(DCMI_CR_ENABLE | DCMI_CR_CAPTURE);
   
  /* Reset the old DCMI configuration */
  temp = DCMI->CR;
  
  temp &= ~((uint32_t)DCMI_CR_CM     | DCMI_CR_ESS   | DCMI_CR_PCKPOL |
                      DCMI_CR_HSPOL  | DCMI_CR_VSPOL | DCMI_CR_FCRC_0 | 
                      DCMI_CR_FCRC_1 | DCMI_CR_EDM_0 | DCMI_CR_EDM_1); 
                  
  /* Sets the new configuration of the DCMI peripheral */
  temp |= ((uint32_t)DCMI_InitStruct->DCMI_CaptureMode |
                     DCMI_InitStruct->DCMI_SynchroMode |
                     DCMI_InitStruct->DCMI_PCKPolarity |
                     DCMI_InitStruct->DCMI_VSPolarity |
                     DCMI_InitStruct->DCMI_HSPolarity |
                     DCMI_InitStruct->DCMI_CaptureRate |
                     DCMI_InitStruct->DCMI_ExtendedDataMode);

  DCMI->CR = temp;                              
}

/**
  * @brief  Fills each DCMI_InitStruct member with its default value.
  * @param  DCMI_InitStruct : pointer to a DCMI_InitTypeDef structure which will
  *         be initialized.
  * @retval None
  */
void DCMI_StructInit(DCMI_InitTypeDef* DCMI_InitStruct)
{
  /* Set the default configuration */
  DCMI_InitStruct->DCMI_CaptureMode = DCMI_CaptureMode_Continuous;
  DCMI_InitStruct->DCMI_SynchroMode = DCMI_SynchroMode_Hardware;
  DCMI_InitStruct->DCMI_PCKPolarity = DCMI_PCKPolarity_Falling;
  DCMI_InitStruct->DCMI_VSPolarity = DCMI_VSPolarity_Low;
  DCMI_InitStruct->DCMI_HSPolarity = DCMI_HSPolarity_Low;
  DCMI_InitStruct->DCMI_CaptureRate = DCMI_CaptureRate_All_Frame;
  DCMI_InitStruct->DCMI_ExtendedDataMode = DCMI_ExtendedDataMode_8b;
}

/**
  * @brief  Initializes the DCMI peripheral CROP mode according to the specified
  *         parameters in the DCMI_CROPInitStruct.
  * @note   This function should be called before to enable and start the DCMI interface.   
  * @param  DCMI_CROPInitStruct:  pointer to a DCMI_CROPInitTypeDef structure that 
  *         contains the configuration information for the DCMI peripheral CROP mode.
  * @retval None
  */
void DCMI_CROPConfig(DCMI_CROPInitTypeDef* DCMI_CROPInitStruct)
{  
  /* Sets the CROP window coordinates */
  DCMI->CWSTRTR = (uint32_t)((uint32_t)DCMI_CROPInitStruct->DCMI_HorizontalOffsetCount |
                  ((uint32_t)DCMI_CROPInitStruct->DCMI_VerticalStartLine << 16));

  /* Sets the CROP window size */
  DCMI->CWSIZER = (uint32_t)(DCMI_CROPInitStruct->DCMI_CaptureCount |
                  ((uint32_t)DCMI_CROPInitStruct->DCMI_VerticalLineCount << 16));
}

/**
  * @brief  Enables or disables the DCMI Crop feature.
  * @note   This function should be called before to enable and start the DCMI interface.
  * @param  NewState: new state of the DCMI Crop feature. 
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void DCMI_CROPCmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
    
  if (NewState != DISABLE)
  {
    /* Enable the DCMI Crop feature */
    DCMI->CR |= (uint32_t)DCMI_CR_CROP;
  }
  else
  {
    /* Disable the DCMI Crop feature */
    DCMI->CR &= ~(uint32_t)DCMI_CR_CROP;
  }
}

/**
  * @brief  Sets the embedded synchronization codes
  * @param  DCMI_CodesInitTypeDef: pointer to a DCMI_CodesInitTypeDef structure that
  *         contains the embedded synchronization codes for the DCMI peripheral.
  * @retval None
  */
void DCMI_SetEmbeddedSynchroCodes(DCMI_CodesInitTypeDef* DCMI_CodesInitStruct)
{
  DCMI->ESCR = (uint32_t)(DCMI_CodesInitStruct->DCMI_FrameStartCode |
                          ((uint32_t)DCMI_CodesInitStruct->DCMI_LineStartCode << 8)|
                          ((uint32_t)DCMI_CodesInitStruct->DCMI_LineEndCode << 16)|
                          ((uint32_t)DCMI_CodesInitStruct->DCMI_FrameEndCode << 24));
}

/**
  * @brief  Enables or disables the DCMI JPEG format.
  * @note   The Crop and Embedded Synchronization features cannot be used in this mode.  
  * @param  NewState: new state of the DCMI JPEG format. 
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void DCMI_JPEGCmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
 
  if (NewState != DISABLE)
  {
    /* Enable the DCMI JPEG format */
    DCMI->CR |= (uint32_t)DCMI_CR_JPEG;
  }
  else
  {
    /* Disable the DCMI JPEG format */
    DCMI->CR &= ~(uint32_t)DCMI_CR_JPEG;
  }
}
/**
  * @}
  */

/** @defgroup DCMI_Group2 Image capture functions
 *  @brief   Image capture functions
 *
@verbatim   
 ===============================================================================
                                 Image capture functions
 ===============================================================================  

@endverbatim
  * @{
  */
  
/**
  * @brief  Enables or disables the DCMI interface.
  * @param  NewState: new state of the DCMI interface. 
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void DCMI_Cmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  if (NewState != DISABLE)
  {
    /* Enable the DCMI by setting ENABLE bit */
    DCMI->CR |= (uint32_t)DCMI_CR_ENABLE;
  }
  else
  {
    /* Disable the DCMI by clearing ENABLE bit */
    DCMI->CR &= ~(uint32_t)DCMI_CR_ENABLE;
  }
}

/**
  * @brief  Enables or disables the DCMI Capture.
  * @param  NewState: new state of the DCMI capture. 
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void DCMI_CaptureCmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
    
  if (NewState != DISABLE)
  {
    /* Enable the DCMI Capture */
    DCMI->CR |= (uint32_t)DCMI_CR_CAPTURE;
  }
  else
  {
    /* Disable the DCMI Capture */
    DCMI->CR &= ~(uint32_t)DCMI_CR_CAPTURE;
  }
}

/**
  * @brief  Reads the data stored in the DR register.
  * @param  None 
  * @retval Data register value
  */
uint32_t DCMI_ReadData(void)
{
  return DCMI->DR;
}
/**
  * @}
  */

/** @defgroup DCMI_Group3 Interrupts and flags management functions
 *  @brief   Interrupts and flags management functions
 *
@verbatim   
 ===============================================================================
                  Interrupts and flags management functions
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Enables or disables the DCMI interface interrupts.
  * @param  DCMI_IT: specifies the DCMI interrupt sources to be enabled or disabled. 
  *          This parameter can be any combination of the following values:
  *            @arg DCMI_IT_FRAME: Frame capture complete interrupt mask
  *            @arg DCMI_IT_OVF: Overflow interrupt mask
  *            @arg DCMI_IT_ERR: Synchronization error interrupt mask
  *            @arg DCMI_IT_VSYNC: VSYNC interrupt mask
  *            @arg DCMI_IT_LINE: Line interrupt mask
  * @param  NewState: new state of the specified DCMI interrupts.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void DCMI_ITConfig(uint16_t DCMI_IT, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_DCMI_CONFIG_IT(DCMI_IT));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  if (NewState != DISABLE)
  {
    /* Enable the Interrupt sources */
    DCMI->IER |= DCMI_IT;
  }
  else
  {
    /* Disable the Interrupt sources */
    DCMI->IER &= (uint16_t)(~DCMI_IT);
  }  
}

/**
  * @brief  Checks whether the  DCMI interface flag is set or not.
  * @param  DCMI_FLAG: specifies the flag to check.
  *          This parameter can be one of the following values:
  *            @arg DCMI_FLAG_FRAMERI: Frame capture complete Raw flag mask
  *            @arg DCMI_FLAG_OVFRI: Overflow Raw flag mask
  *            @arg DCMI_FLAG_ERRRI: Synchronization error Raw flag mask
  *            @arg DCMI_FLAG_VSYNCRI: VSYNC Raw flag mask
  *            @arg DCMI_FLAG_LINERI: Line Raw flag mask
  *            @arg DCMI_FLAG_FRAMEMI: Frame capture complete Masked flag mask
  *            @arg DCMI_FLAG_OVFMI: Overflow Masked flag mask
  *            @arg DCMI_FLAG_ERRMI: Synchronization error Masked flag mask
  *            @arg DCMI_FLAG_VSYNCMI: VSYNC Masked flag mask
  *            @arg DCMI_FLAG_LINEMI: Line Masked flag mask
  *            @arg DCMI_FLAG_HSYNC: HSYNC flag mask
  *            @arg DCMI_FLAG_VSYNC: VSYNC flag mask
  *            @arg DCMI_FLAG_FNE: Fifo not empty flag mask
  * @retval The new state of DCMI_FLAG (SET or RESET).
  */
FlagStatus DCMI_GetFlagStatus(uint16_t DCMI_FLAG)
{
  FlagStatus bitstatus = RESET;
  uint32_t dcmireg, tempreg = 0;

  /* Check the parameters */
  assert_param(IS_DCMI_GET_FLAG(DCMI_FLAG));
  
  /* Get the DCMI register index */
  dcmireg = (((uint16_t)DCMI_FLAG) >> 12);
  
  if (dcmireg == 0x01) /* The FLAG is in RISR register */
  {
    tempreg= DCMI->RISR;
  }
  else if (dcmireg == 0x02) /* The FLAG is in SR register */
  {
    tempreg = DCMI->SR;
  }
  else /* The FLAG is in MISR register */
  {
    tempreg = DCMI->MISR;
  }
  
  if ((tempreg & DCMI_FLAG) != (uint16_t)RESET )
  {
    bitstatus = SET;
  }
  else
  {
    bitstatus = RESET;
  }
  /* Return the DCMI_FLAG status */
  return  bitstatus;
}

/**
  * @brief  Clears the DCMI's pending flags.
  * @param  DCMI_FLAG: specifies the flag to clear.
  *          This parameter can be any combination of the following values:
  *            @arg DCMI_FLAG_FRAMERI: Frame capture complete Raw flag mask
  *            @arg DCMI_FLAG_OVFRI: Overflow Raw flag mask
  *            @arg DCMI_FLAG_ERRRI: Synchronization error Raw flag mask
  *            @arg DCMI_FLAG_VSYNCRI: VSYNC Raw flag mask
  *            @arg DCMI_FLAG_LINERI: Line Raw flag mask
  * @retval None
  */
void DCMI_ClearFlag(uint16_t DCMI_FLAG)
{
  /* Check the parameters */
  assert_param(IS_DCMI_CLEAR_FLAG(DCMI_FLAG));
  
  /* Clear the flag by writing in the ICR register 1 in the corresponding 
  Flag position*/
  
  DCMI->ICR = DCMI_FLAG;
}

/**
  * @brief  Checks whether the DCMI interrupt has occurred or not.
  * @param  DCMI_IT: specifies the DCMI interrupt source to check.
  *          This parameter can be one of the following values:
  *            @arg DCMI_IT_FRAME: Frame capture complete interrupt mask
  *            @arg DCMI_IT_OVF: Overflow interrupt mask
  *            @arg DCMI_IT_ERR: Synchronization error interrupt mask
  *            @arg DCMI_IT_VSYNC: VSYNC interrupt mask
  *            @arg DCMI_IT_LINE: Line interrupt mask
  * @retval The new state of DCMI_IT (SET or RESET).
  */
ITStatus DCMI_GetITStatus(uint16_t DCMI_IT)
{
  ITStatus bitstatus = RESET;
  uint32_t itstatus = 0;
  
  /* Check the parameters */
  assert_param(IS_DCMI_GET_IT(DCMI_IT));
  
  itstatus = DCMI->MISR & DCMI_IT; /* Only masked interrupts are checked */
  
  if ((itstatus != (uint16_t)RESET))
  {
    bitstatus = SET;
  }
  else
  {
    bitstatus = RESET;
  }
  return bitstatus;
}

/**
  * @brief  Clears the DCMI's interrupt pending bits.
  * @param  DCMI_IT: specifies the DCMI interrupt pending bit to clear.
  *          This parameter can be any combination of the following values:
  *            @arg DCMI_IT_FRAME: Frame capture complete interrupt mask
  *            @arg DCMI_IT_OVF: Overflow interrupt mask
  *            @arg DCMI_IT_ERR: Synchronization error interrupt mask
  *            @arg DCMI_IT_VSYNC: VSYNC interrupt mask
  *            @arg DCMI_IT_LINE: Line interrupt mask
  * @retval None
  */
void DCMI_ClearITPendingBit(uint16_t DCMI_IT)
{
  /* Clear the interrupt pending Bit by writing in the ICR register 1 in the 
  corresponding pending Bit position*/
  
  DCMI->ICR = DCMI_IT;
}
/**
  * @}
  */ 

/**
  * @}
  */ 

/**
  * @}
  */ 

/**
  * @}
  */ 

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/
