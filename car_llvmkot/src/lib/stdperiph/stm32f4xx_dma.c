/**
  ******************************************************************************
  * @file    stm32f4xx_dma.c
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file provides firmware functions to manage the following 
  *          functionalities of the Direct Memory Access controller (DMA):           
  *           - Initialization and Configuration
  *           - Data Counter
  *           - Double Buffer mode configuration and command  
  *           - Interrupts and flags management
  *           
  *  @verbatim
  *      
  *          ===================================================================      
  *                                 How to use this driver
  *          =================================================================== 
  *          1. Enable The DMA controller clock using RCC_AHB1PeriphResetCmd(RCC_AHB1Periph_DMA1, ENABLE)
  *             function for DMA1 or using RCC_AHB1PeriphResetCmd(RCC_AHB1Periph_DMA2, ENABLE)
  *             function for DMA2.
  *
  *          2. Enable and configure the peripheral to be connected to the DMA Stream
  *             (except for internal SRAM / FLASH memories: no initialization is 
  *             necessary). 
  *        
  *          3. For a given Stream, program the required configuration through following parameters:   
  *             Source and Destination addresses, Transfer Direction, Transfer size, Source and Destination 
  *             data formats, Circular or Normal mode, Stream Priority level, Source and Destination 
  *             Incrementation mode, FIFO mode and its Threshold (if needed), Burst mode for Source and/or 
  *             Destination (if needed) using the DMA_Init() function.
  *             To avoid filling un-nesecessary fields, you can call DMA_StructInit() function
  *             to initialize a given structure with default values (reset values), the modify
  *             only necessary fields (ie. Source and Destination addresses, Transfer size and Data Formats).
  *
  *          4. Enable the NVIC and the corresponding interrupt(s) using the function 
  *             DMA_ITConfig() if you need to use DMA interrupts. 
  *
  *          5. Optionally, if the Circular mode is enabled, you can use the Double buffer mode by configuring 
  *             the second Memory address and the first Memory to be used through the function 
  *             DMA_DoubleBufferModeConfig(). Then enable the Double buffer mode through the function
  *             DMA_DoubleBufferModeCmd(). These operations must be done before step 6.
  *    
  *          6. Enable the DMA stream using the DMA_Cmd() function. 
  *                
  *          7. Activate the needed Stream Request using PPP_DMACmd() function for
  *             any PPP peripheral except internal SRAM and FLASH (ie. SPI, USART ...)
  *             The function allowing this operation is provided in each PPP peripheral
  *             driver (ie. SPI_DMACmd for SPI peripheral).
  *             Once the Stream is enabled, it is not possible to modify its configuration
  *             unless the stream is stopped and disabled.
  *             After enabling the Stream, it is advised to monitor the EN bit status using
  *             the function DMA_GetCmdStatus(). In case of configuration errors or bus errors
  *             this bit will remain reset and all transfers on this Stream will remain on hold.      
  *
  *          8. Optionally, you can configure the number of data to be transferred
  *             when the Stream is disabled (ie. after each Transfer Complete event
  *             or when a Transfer Error occurs) using the function DMA_SetCurrDataCounter().
  *             And you can get the number of remaining data to be transferred using 
  *             the function DMA_GetCurrDataCounter() at run time (when the DMA Stream is
  *             enabled and running).  
  *                   
  *          9. To control DMA events you can use one of the following 
  *              two methods:
  *               a- Check on DMA Stream flags using the function DMA_GetFlagStatus().  
  *               b- Use DMA interrupts through the function DMA_ITConfig() at initialization
  *                  phase and DMA_GetITStatus() function into interrupt routines in
  *                  communication phase.  
  *              After checking on a flag you should clear it using DMA_ClearFlag()
  *              function. And after checking on an interrupt event you should 
  *              clear it using DMA_ClearITPendingBit() function.    
  *              
  *          10. Optionally, if Circular mode and Double Buffer mode are enabled, you can modify
  *              the Memory Addresses using the function DMA_MemoryTargetConfig(). Make sure that
  *              the Memory Address to be modified is not the one currently in use by DMA Stream.
  *              This condition can be monitored using the function DMA_GetCurrentMemoryTarget().
  *              
  *          11. Optionally, Pause-Resume operations may be performed:
  *              The DMA_Cmd() function may be used to perform Pause-Resume operation. When a 
  *              transfer is ongoing, calling this function to disable the Stream will cause the 
  *              transfer to be paused. All configuration registers and the number of remaining 
  *              data will be preserved. When calling again this function to re-enable the Stream, 
  *              the transfer will be resumed from the point where it was paused.          
  *                 
  * @note   Memory-to-Memory transfer is possible by setting the address of the memory into
  *         the Peripheral registers. In this mode, Circular mode and Double Buffer mode
  *         are not allowed.
  *  
  * @note   The FIFO is used mainly to reduce bus usage and to allow data packing/unpacking: it is
  *         possible to set different Data Sizes for the Peripheral and the Memory (ie. you can set
  *         Half-Word data size for the peripheral to access its data register and set Word data size
  *         for the Memory to gain in access time. Each two Half-words will be packed and written in
  *         a single access to a Word in the Memory).
  *    
  * @note  When FIFO is disabled, it is not allowed to configure different Data Sizes for Source
  *        and Destination. In this case the Peripheral Data Size will be applied to both Source
  *        and Destination.               
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
#include "stm32f4xx_dma.h"
#include "stm32f4xx_rcc.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @defgroup DMA 
  * @brief DMA driver modules
  * @{
  */ 

/* Private typedef -----------------------------------------------------------*/
/* Private define ------------------------------------------------------------*/

/* Masks Definition */
#define TRANSFER_IT_ENABLE_MASK (uint32_t)(DMA_SxCR_TCIE | DMA_SxCR_HTIE | \
                                           DMA_SxCR_TEIE | DMA_SxCR_DMEIE)

#define DMA_Stream0_IT_MASK     (uint32_t)(DMA_LISR_FEIF0 | DMA_LISR_DMEIF0 | \
                                           DMA_LISR_TEIF0 | DMA_LISR_HTIF0 | \
                                           DMA_LISR_TCIF0)

#define DMA_Stream1_IT_MASK     (uint32_t)(DMA_Stream0_IT_MASK << 6)
#define DMA_Stream2_IT_MASK     (uint32_t)(DMA_Stream0_IT_MASK << 16)
#define DMA_Stream3_IT_MASK     (uint32_t)(DMA_Stream0_IT_MASK << 22)
#define DMA_Stream4_IT_MASK     (uint32_t)(DMA_Stream0_IT_MASK | (uint32_t)0x20000000)
#define DMA_Stream5_IT_MASK     (uint32_t)(DMA_Stream1_IT_MASK | (uint32_t)0x20000000)
#define DMA_Stream6_IT_MASK     (uint32_t)(DMA_Stream2_IT_MASK | (uint32_t)0x20000000)
#define DMA_Stream7_IT_MASK     (uint32_t)(DMA_Stream3_IT_MASK | (uint32_t)0x20000000)
#define TRANSFER_IT_MASK        (uint32_t)0x0F3C0F3C
#define HIGH_ISR_MASK           (uint32_t)0x20000000
#define RESERVED_MASK           (uint32_t)0x0F7D0F7D  

/* Private macro -------------------------------------------------------------*/
/* Private variables ---------------------------------------------------------*/
/* Private function prototypes -----------------------------------------------*/
/* Private functions ---------------------------------------------------------*/


/** @defgroup DMA_Private_Functions
  * @{
  */

/** @defgroup DMA_Group1 Initialization and Configuration functions
 *  @brief   Initialization and Configuration functions
 *
@verbatim   
 ===============================================================================
                 Initialization and Configuration functions
 ===============================================================================  

  This subsection provides functions allowing to initialize the DMA Stream source
  and destination addresses, incrementation and data sizes, transfer direction, 
  buffer size, circular/normal mode selection, memory-to-memory mode selection 
  and Stream priority value.
  
  The DMA_Init() function follows the DMA configuration procedures as described in
  reference manual (RM0090) except the first point: waiting on EN bit to be reset.
  This condition should be checked by user application using the function DMA_GetCmdStatus()
  before calling the DMA_Init() function.

@endverbatim
  * @{
  */

/**
  * @brief  Deinitialize the DMAy Streamx registers to their default reset values.
  * @param  DMAy_Streamx: where y can be 1 or 2 to select the DMA and x can be 0
  *         to 7 to select the DMA Stream.
  * @retval None
  */
void DMA_DeInit(DMA_Stream_TypeDef* DMAy_Streamx)
{
  /* Check the parameters */
  assert_param(IS_DMA_ALL_PERIPH(DMAy_Streamx));

  /* Disable the selected DMAy Streamx */
  DMAy_Streamx->CR &= ~((uint32_t)DMA_SxCR_EN);

  /* Reset DMAy Streamx control register */
  DMAy_Streamx->CR  = 0;
  
  /* Reset DMAy Streamx Number of Data to Transfer register */
  DMAy_Streamx->NDTR = 0;
  
  /* Reset DMAy Streamx peripheral address register */
  DMAy_Streamx->PAR  = 0;
  
  /* Reset DMAy Streamx memory 0 address register */
  DMAy_Streamx->M0AR = 0;

  /* Reset DMAy Streamx memory 1 address register */
  DMAy_Streamx->M1AR = 0;

  /* Reset DMAy Streamx FIFO control register */
  DMAy_Streamx->FCR = (uint32_t)0x00000021; 

  /* Reset interrupt pending bits for the selected stream */
  if (DMAy_Streamx == DMA1_Stream0)
  {
    /* Reset interrupt pending bits for DMA1 Stream0 */
    DMA1->LIFCR = DMA_Stream0_IT_MASK;
  }
  else if (DMAy_Streamx == DMA1_Stream1)
  {
    /* Reset interrupt pending bits for DMA1 Stream1 */
    DMA1->LIFCR = DMA_Stream1_IT_MASK;
  }
  else if (DMAy_Streamx == DMA1_Stream2)
  {
    /* Reset interrupt pending bits for DMA1 Stream2 */
    DMA1->LIFCR = DMA_Stream2_IT_MASK;
  }
  else if (DMAy_Streamx == DMA1_Stream3)
  {
    /* Reset interrupt pending bits for DMA1 Stream3 */
    DMA1->LIFCR = DMA_Stream3_IT_MASK;
  }
  else if (DMAy_Streamx == DMA1_Stream4)
  {
    /* Reset interrupt pending bits for DMA1 Stream4 */
    DMA1->HIFCR = DMA_Stream4_IT_MASK;
  }
  else if (DMAy_Streamx == DMA1_Stream5)
  {
    /* Reset interrupt pending bits for DMA1 Stream5 */
    DMA1->HIFCR = DMA_Stream5_IT_MASK;
  }
  else if (DMAy_Streamx == DMA1_Stream6)
  {
    /* Reset interrupt pending bits for DMA1 Stream6 */
    DMA1->HIFCR = (uint32_t)DMA_Stream6_IT_MASK;
  }
  else if (DMAy_Streamx == DMA1_Stream7)
  {
    /* Reset interrupt pending bits for DMA1 Stream7 */
    DMA1->HIFCR = DMA_Stream7_IT_MASK;
  }
  else if (DMAy_Streamx == DMA2_Stream0)
  {
    /* Reset interrupt pending bits for DMA2 Stream0 */
    DMA2->LIFCR = DMA_Stream0_IT_MASK;
  }
  else if (DMAy_Streamx == DMA2_Stream1)
  {
    /* Reset interrupt pending bits for DMA2 Stream1 */
    DMA2->LIFCR = DMA_Stream1_IT_MASK;
  }
  else if (DMAy_Streamx == DMA2_Stream2)
  {
    /* Reset interrupt pending bits for DMA2 Stream2 */
    DMA2->LIFCR = DMA_Stream2_IT_MASK;
  }
  else if (DMAy_Streamx == DMA2_Stream3)
  {
    /* Reset interrupt pending bits for DMA2 Stream3 */
    DMA2->LIFCR = DMA_Stream3_IT_MASK;
  }
  else if (DMAy_Streamx == DMA2_Stream4)
  {
    /* Reset interrupt pending bits for DMA2 Stream4 */
    DMA2->HIFCR = DMA_Stream4_IT_MASK;
  }
  else if (DMAy_Streamx == DMA2_Stream5)
  {
    /* Reset interrupt pending bits for DMA2 Stream5 */
    DMA2->HIFCR = DMA_Stream5_IT_MASK;
  }
  else if (DMAy_Streamx == DMA2_Stream6)
  {
    /* Reset interrupt pending bits for DMA2 Stream6 */
    DMA2->HIFCR = DMA_Stream6_IT_MASK;
  }
  else 
  {
    if (DMAy_Streamx == DMA2_Stream7)
    {
      /* Reset interrupt pending bits for DMA2 Stream7 */
      DMA2->HIFCR = DMA_Stream7_IT_MASK;
    }
  }
}

/**
  * @brief  Initializes the DMAy Streamx according to the specified parameters in 
  *         the DMA_InitStruct structure.
  * @note   Before calling this function, it is recommended to check that the Stream 
  *         is actually disabled using the function DMA_GetCmdStatus().  
  * @param  DMAy_Streamx: where y can be 1 or 2 to select the DMA and x can be 0
  *         to 7 to select the DMA Stream.
  * @param  DMA_InitStruct: pointer to a DMA_InitTypeDef structure that contains
  *         the configuration information for the specified DMA Stream.  
  * @retval None
  */
void DMA_Init(DMA_Stream_TypeDef* DMAy_Streamx, DMA_InitTypeDef* DMA_InitStruct)
{
  uint32_t tmpreg = 0;

  /* Check the parameters */
  assert_param(IS_DMA_ALL_PERIPH(DMAy_Streamx));
  assert_param(IS_DMA_CHANNEL(DMA_InitStruct->DMA_Channel));
  assert_param(IS_DMA_DIRECTION(DMA_InitStruct->DMA_DIR));
  assert_param(IS_DMA_BUFFER_SIZE(DMA_InitStruct->DMA_BufferSize));
  assert_param(IS_DMA_PERIPHERAL_INC_STATE(DMA_InitStruct->DMA_PeripheralInc));
  assert_param(IS_DMA_MEMORY_INC_STATE(DMA_InitStruct->DMA_MemoryInc));
  assert_param(IS_DMA_PERIPHERAL_DATA_SIZE(DMA_InitStruct->DMA_PeripheralDataSize));
  assert_param(IS_DMA_MEMORY_DATA_SIZE(DMA_InitStruct->DMA_MemoryDataSize));
  assert_param(IS_DMA_MODE(DMA_InitStruct->DMA_Mode));
  assert_param(IS_DMA_PRIORITY(DMA_InitStruct->DMA_Priority));
  assert_param(IS_DMA_FIFO_MODE_STATE(DMA_InitStruct->DMA_FIFOMode));
  assert_param(IS_DMA_FIFO_THRESHOLD(DMA_InitStruct->DMA_FIFOThreshold));
  assert_param(IS_DMA_MEMORY_BURST(DMA_InitStruct->DMA_MemoryBurst));
  assert_param(IS_DMA_PERIPHERAL_BURST(DMA_InitStruct->DMA_PeripheralBurst));

  /*------------------------- DMAy Streamx CR Configuration ------------------*/
  /* Get the DMAy_Streamx CR value */
  tmpreg = DMAy_Streamx->CR;

  /* Clear CHSEL, MBURST, PBURST, PL, MSIZE, PSIZE, MINC, PINC, CIRC and DIR bits */
  tmpreg &= ((uint32_t)~(DMA_SxCR_CHSEL | DMA_SxCR_MBURST | DMA_SxCR_PBURST | \
                         DMA_SxCR_PL | DMA_SxCR_MSIZE | DMA_SxCR_PSIZE | \
                         DMA_SxCR_MINC | DMA_SxCR_PINC | DMA_SxCR_CIRC | \
                         DMA_SxCR_DIR));

  /* Configure DMAy Streamx: */
  /* Set CHSEL bits according to DMA_CHSEL value */
  /* Set DIR bits according to DMA_DIR value */
  /* Set PINC bit according to DMA_PeripheralInc value */
  /* Set MINC bit according to DMA_MemoryInc value */
  /* Set PSIZE bits according to DMA_PeripheralDataSize value */
  /* Set MSIZE bits according to DMA_MemoryDataSize value */
  /* Set CIRC bit according to DMA_Mode value */
  /* Set PL bits according to DMA_Priority value */
  /* Set MBURST bits according to DMA_MemoryBurst value */
  /* Set PBURST bits according to DMA_PeripheralBurst value */
  tmpreg |= DMA_InitStruct->DMA_Channel | DMA_InitStruct->DMA_DIR |
            DMA_InitStruct->DMA_PeripheralInc | DMA_InitStruct->DMA_MemoryInc |
            DMA_InitStruct->DMA_PeripheralDataSize | DMA_InitStruct->DMA_MemoryDataSize |
            DMA_InitStruct->DMA_Mode | DMA_InitStruct->DMA_Priority |
            DMA_InitStruct->DMA_MemoryBurst | DMA_InitStruct->DMA_PeripheralBurst;

  /* Write to DMAy Streamx CR register */
  DMAy_Streamx->CR = tmpreg;

  /*------------------------- DMAy Streamx FCR Configuration -----------------*/
  /* Get the DMAy_Streamx FCR value */
  tmpreg = DMAy_Streamx->FCR;

  /* Clear DMDIS and FTH bits */
  tmpreg &= (uint32_t)~(DMA_SxFCR_DMDIS | DMA_SxFCR_FTH);

  /* Configure DMAy Streamx FIFO: 
    Set DMDIS bits according to DMA_FIFOMode value 
    Set FTH bits according to DMA_FIFOThreshold value */
  tmpreg |= DMA_InitStruct->DMA_FIFOMode | DMA_InitStruct->DMA_FIFOThreshold;

  /* Write to DMAy Streamx CR */
  DMAy_Streamx->FCR = tmpreg;

  /*------------------------- DMAy Streamx NDTR Configuration ----------------*/
  /* Write to DMAy Streamx NDTR register */
  DMAy_Streamx->NDTR = DMA_InitStruct->DMA_BufferSize;

  /*------------------------- DMAy Streamx PAR Configuration -----------------*/
  /* Write to DMAy Streamx PAR */
  DMAy_Streamx->PAR = DMA_InitStruct->DMA_PeripheralBaseAddr;

  /*------------------------- DMAy Streamx M0AR Configuration ----------------*/
  /* Write to DMAy Streamx M0AR */
  DMAy_Streamx->M0AR = DMA_InitStruct->DMA_Memory0BaseAddr;
}

/**
  * @brief  Fills each DMA_InitStruct member with its default value.
  * @param  DMA_InitStruct : pointer to a DMA_InitTypeDef structure which will 
  *         be initialized.
  * @retval None
  */
void DMA_StructInit(DMA_InitTypeDef* DMA_InitStruct)
{
  /*-------------- Reset DMA init structure parameters values ----------------*/
  /* Initialize the DMA_Channel member */
  DMA_InitStruct->DMA_Channel = 0;

  /* Initialize the DMA_PeripheralBaseAddr member */
  DMA_InitStruct->DMA_PeripheralBaseAddr = 0;

  /* Initialize the DMA_Memory0BaseAddr member */
  DMA_InitStruct->DMA_Memory0BaseAddr = 0;

  /* Initialize the DMA_DIR member */
  DMA_InitStruct->DMA_DIR = DMA_DIR_PeripheralToMemory;

  /* Initialize the DMA_BufferSize member */
  DMA_InitStruct->DMA_BufferSize = 0;

  /* Initialize the DMA_PeripheralInc member */
  DMA_InitStruct->DMA_PeripheralInc = DMA_PeripheralInc_Disable;

  /* Initialize the DMA_MemoryInc member */
  DMA_InitStruct->DMA_MemoryInc = DMA_MemoryInc_Disable;

  /* Initialize the DMA_PeripheralDataSize member */
  DMA_InitStruct->DMA_PeripheralDataSize = DMA_PeripheralDataSize_Byte;

  /* Initialize the DMA_MemoryDataSize member */
  DMA_InitStruct->DMA_MemoryDataSize = DMA_MemoryDataSize_Byte;

  /* Initialize the DMA_Mode member */
  DMA_InitStruct->DMA_Mode = DMA_Mode_Normal;

  /* Initialize the DMA_Priority member */
  DMA_InitStruct->DMA_Priority = DMA_Priority_Low;

  /* Initialize the DMA_FIFOMode member */
  DMA_InitStruct->DMA_FIFOMode = DMA_FIFOMode_Disable;

  /* Initialize the DMA_FIFOThreshold member */
  DMA_InitStruct->DMA_FIFOThreshold = DMA_FIFOThreshold_1QuarterFull;

  /* Initialize the DMA_MemoryBurst member */
  DMA_InitStruct->DMA_MemoryBurst = DMA_MemoryBurst_Single;

  /* Initialize the DMA_PeripheralBurst member */
  DMA_InitStruct->DMA_PeripheralBurst = DMA_PeripheralBurst_Single;
}

/**
  * @brief  Enables or disables the specified DMAy Streamx.
  * @param  DMAy_Streamx: where y can be 1 or 2 to select the DMA and x can be 0
  *         to 7 to select the DMA Stream.
  * @param  NewState: new state of the DMAy Streamx. 
  *          This parameter can be: ENABLE or DISABLE.
  *
  * @note  This function may be used to perform Pause-Resume operation. When a
  *        transfer is ongoing, calling this function to disable the Stream will
  *        cause the transfer to be paused. All configuration registers and the
  *        number of remaining data will be preserved. When calling again this
  *        function to re-enable the Stream, the transfer will be resumed from
  *        the point where it was paused.          
  *    
  * @note  After configuring the DMA Stream (DMA_Init() function) and enabling the
  *        stream, it is recommended to check (or wait until) the DMA Stream is
  *        effectively enabled. A Stream may remain disabled if a configuration 
  *        parameter is wrong.
  *        After disabling a DMA Stream, it is also recommended to check (or wait
  *        until) the DMA Stream is effectively disabled. If a Stream is disabled 
  *        while a data transfer is ongoing, the current data will be transferred
  *        and the Stream will be effectively disabled only after the transfer of
  *        this single data is finished.            
  *    
  * @retval None
  */
void DMA_Cmd(DMA_Stream_TypeDef* DMAy_Streamx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_DMA_ALL_PERIPH(DMAy_Streamx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  if (NewState != DISABLE)
  {
    /* Enable the selected DMAy Streamx by setting EN bit */
    DMAy_Streamx->CR |= (uint32_t)DMA_SxCR_EN;
  }
  else
  {
    /* Disable the selected DMAy Streamx by clearing EN bit */
    DMAy_Streamx->CR &= ~(uint32_t)DMA_SxCR_EN;
  }
}

/**
  * @brief  Configures, when the PINC (Peripheral Increment address mode) bit is
  *         set, if the peripheral address should be incremented with the data 
  *         size (configured with PSIZE bits) or by a fixed offset equal to 4
  *         (32-bit aligned addresses).
  *   
  * @note   This function has no effect if the Peripheral Increment mode is disabled.
  *     
  * @param  DMAy_Streamx: where y can be 1 or 2 to select the DMA and x can be 0
  *          to 7 to select the DMA Stream.
  * @param  DMA_Pincos: specifies the Peripheral increment offset size.
  *          This parameter can be one of the following values:
  *            @arg DMA_PINCOS_Psize: Peripheral address increment is done  
  *                                   accordingly to PSIZE parameter.
  *            @arg DMA_PINCOS_WordAligned: Peripheral address increment offset is 
  *                                         fixed to 4 (32-bit aligned addresses). 
  * @retval None
  */
void DMA_PeriphIncOffsetSizeConfig(DMA_Stream_TypeDef* DMAy_Streamx, uint32_t DMA_Pincos)
{
  /* Check the parameters */
  assert_param(IS_DMA_ALL_PERIPH(DMAy_Streamx));
  assert_param(IS_DMA_PINCOS_SIZE(DMA_Pincos));

  /* Check the needed Peripheral increment offset */
  if(DMA_Pincos != DMA_PINCOS_Psize)
  {
    /* Configure DMA_SxCR_PINCOS bit with the input parameter */
    DMAy_Streamx->CR |= (uint32_t)DMA_SxCR_PINCOS;     
  }
  else
  {
    /* Clear the PINCOS bit: Peripheral address incremented according to PSIZE */
    DMAy_Streamx->CR &= ~(uint32_t)DMA_SxCR_PINCOS;    
  }
}

/**
  * @brief  Configures, when the DMAy Streamx is disabled, the flow controller for
  *         the next transactions (Peripheral or Memory).
  *       
  * @note   Before enabling this feature, check if the used peripheral supports 
  *         the Flow Controller mode or not.    
  *  
  * @param  DMAy_Streamx: where y can be 1 or 2 to select the DMA and x can be 0
  *          to 7 to select the DMA Stream.
  * @param  DMA_FlowCtrl: specifies the DMA flow controller.
  *          This parameter can be one of the following values:
  *            @arg DMA_FlowCtrl_Memory: DMAy_Streamx transactions flow controller is 
  *                                      the DMA controller.
  *            @arg DMA_FlowCtrl_Peripheral: DMAy_Streamx transactions flow controller 
  *                                          is the peripheral.    
  * @retval None
  */
void DMA_FlowControllerConfig(DMA_Stream_TypeDef* DMAy_Streamx, uint32_t DMA_FlowCtrl)
{
  /* Check the parameters */
  assert_param(IS_DMA_ALL_PERIPH(DMAy_Streamx));
  assert_param(IS_DMA_FLOW_CTRL(DMA_FlowCtrl));

  /* Check the needed flow controller  */
  if(DMA_FlowCtrl != DMA_FlowCtrl_Memory)
  {
    /* Configure DMA_SxCR_PFCTRL bit with the input parameter */
    DMAy_Streamx->CR |= (uint32_t)DMA_SxCR_PFCTRL;   
  }
  else
  {
    /* Clear the PFCTRL bit: Memory is the flow controller */
    DMAy_Streamx->CR &= ~(uint32_t)DMA_SxCR_PFCTRL;    
  }
}
/**
  * @}
  */

/** @defgroup DMA_Group2 Data Counter functions
 *  @brief   Data Counter functions 
 *
@verbatim   
 ===============================================================================
                           Data Counter functions
 ===============================================================================  

  This subsection provides function allowing to configure and read the buffer size
  (number of data to be transferred). 

  The DMA data counter can be written only when the DMA Stream is disabled 
  (ie. after transfer complete event).

  The following function can be used to write the Stream data counter value:
    - void DMA_SetCurrDataCounter(DMA_Stream_TypeDef* DMAy_Streamx, uint16_t Counter);

@note It is advised to use this function rather than DMA_Init() in situations where
      only the Data buffer needs to be reloaded.

@note If the Source and Destination Data Sizes are different, then the value written in
      data counter, expressing the number of transfers, is relative to the number of 
      transfers from the Peripheral point of view.
      ie. If Memory data size is Word, Peripheral data size is Half-Words, then the value
      to be configured in the data counter is the number of Half-Words to be transferred
      from/to the peripheral.

  The DMA data counter can be read to indicate the number of remaining transfers for
  the relative DMA Stream. This counter is decremented at the end of each data 
  transfer and when the transfer is complete: 
   - If Normal mode is selected: the counter is set to 0.
   - If Circular mode is selected: the counter is reloaded with the initial value
     (configured before enabling the DMA Stream)
   
  The following function can be used to read the Stream data counter value:
     - uint16_t DMA_GetCurrDataCounter(DMA_Stream_TypeDef* DMAy_Streamx);

@endverbatim
  * @{
  */

/**
  * @brief  Writes the number of data units to be transferred on the DMAy Streamx.
  * @param  DMAy_Streamx: where y can be 1 or 2 to select the DMA and x can be 0
  *          to 7 to select the DMA Stream.
  * @param  Counter: Number of data units to be transferred (from 0 to 65535) 
  *          Number of data items depends only on the Peripheral data format.
  *            
  * @note   If Peripheral data format is Bytes: number of data units is equal 
  *         to total number of bytes to be transferred.
  *           
  * @note   If Peripheral data format is Half-Word: number of data units is  
  *         equal to total number of bytes to be transferred / 2.
  *           
  * @note   If Peripheral data format is Word: number of data units is equal 
  *         to total  number of bytes to be transferred / 4.
  *      
  * @note   In Memory-to-Memory transfer mode, the memory buffer pointed by 
  *         DMAy_SxPAR register is considered as Peripheral.
  *      
  * @retval The number of remaining data units in the current DMAy Streamx transfer.
  */
void DMA_SetCurrDataCounter(DMA_Stream_TypeDef* DMAy_Streamx, uint16_t Counter)
{
  /* Check the parameters */
  assert_param(IS_DMA_ALL_PERIPH(DMAy_Streamx));

  /* Write the number of data units to be transferred */
  DMAy_Streamx->NDTR = (uint16_t)Counter;
}

/**
  * @brief  Returns the number of remaining data units in the current DMAy Streamx transfer.
  * @param  DMAy_Streamx: where y can be 1 or 2 to select the DMA and x can be 0
  *          to 7 to select the DMA Stream.
  * @retval The number of remaining data units in the current DMAy Streamx transfer.
  */
uint16_t DMA_GetCurrDataCounter(DMA_Stream_TypeDef* DMAy_Streamx)
{
  /* Check the parameters */
  assert_param(IS_DMA_ALL_PERIPH(DMAy_Streamx));

  /* Return the number of remaining data units for DMAy Streamx */
  return ((uint16_t)(DMAy_Streamx->NDTR));
}
/**
  * @}
  */

/** @defgroup DMA_Group3 Double Buffer mode functions
 *  @brief   Double Buffer mode functions 
 *
@verbatim   
 ===============================================================================
                         Double Buffer mode functions
 ===============================================================================  

  This subsection provides function allowing to configure and control the double 
  buffer mode parameters.
  
  The Double Buffer mode can be used only when Circular mode is enabled.
  The Double Buffer mode cannot be used when transferring data from Memory to Memory.
  
  The Double Buffer mode allows to set two different Memory addresses from/to which
  the DMA controller will access alternatively (after completing transfer to/from target
  memory 0, it will start transfer to/from target memory 1).
  This allows to reduce software overhead for double buffering and reduce the CPU
  access time.

  Two functions must be called before calling the DMA_Init() function:
   - void DMA_DoubleBufferModeConfig(DMA_Stream_TypeDef* DMAy_Streamx, uint32_t Memory1BaseAddr,
                                uint32_t DMA_CurrentMemory);
   - void DMA_DoubleBufferModeCmd(DMA_Stream_TypeDef* DMAy_Streamx, FunctionalState NewState);
   
  DMA_DoubleBufferModeConfig() is called to configure the Memory 1 base address and the first
  Memory target from/to which the transfer will start after enabling the DMA Stream.
  Then DMA_DoubleBufferModeCmd() must be called to enable the Double Buffer mode (or disable 
  it when it should not be used).
  
   
  Two functions can be called dynamically when the transfer is ongoing (or when the DMA Stream is 
  stopped) to modify on of the target Memories addresses or to check wich Memory target is currently
   used:
    - void DMA_MemoryTargetConfig(DMA_Stream_TypeDef* DMAy_Streamx, uint32_t MemoryBaseAddr,
                            uint32_t DMA_MemoryTarget);
    - uint32_t DMA_GetCurrentMemoryTarget(DMA_Stream_TypeDef* DMAy_Streamx);

  DMA_MemoryTargetConfig() can be called to modify the base address of one of the two target Memories.
  The Memory of which the base address will be modified must not be currently be used by the DMA Stream
  (ie. if the DMA Stream is currently transferring from Memory 1 then you can only modify base address
  of target Memory 0 and vice versa).
  To check this condition, it is recommended to use the function DMA_GetCurrentMemoryTarget() which
  returns the index of the Memory target currently in use by the DMA Stream.

@endverbatim
  * @{
  */
  
/**
  * @brief  Configures, when the DMAy Streamx is disabled, the double buffer mode 
  *         and the current memory target.
  * @param  DMAy_Streamx: where y can be 1 or 2 to select the DMA and x can be 0
  *          to 7 to select the DMA Stream.
  * @param  Memory1BaseAddr: the base address of the second buffer (Memory 1)  
  * @param  DMA_CurrentMemory: specifies which memory will be first buffer for
  *         the transactions when the Stream will be enabled. 
  *          This parameter can be one of the following values:
  *            @arg DMA_Memory_0: Memory 0 is the current buffer.
  *            @arg DMA_Memory_1: Memory 1 is the current buffer.  
  *       
  * @note   Memory0BaseAddr is set by the DMA structure configuration in DMA_Init().
  *   
  * @retval None
  */
void DMA_DoubleBufferModeConfig(DMA_Stream_TypeDef* DMAy_Streamx, uint32_t Memory1BaseAddr,
                                uint32_t DMA_CurrentMemory)
{  
  /* Check the parameters */
  assert_param(IS_DMA_ALL_PERIPH(DMAy_Streamx));
  assert_param(IS_DMA_CURRENT_MEM(DMA_CurrentMemory));

  if (DMA_CurrentMemory != DMA_Memory_0)
  {
    /* Set Memory 1 as current memory address */
    DMAy_Streamx->CR |= (uint32_t)(DMA_SxCR_CT);    
  }
  else
  {
    /* Set Memory 0 as current memory address */
    DMAy_Streamx->CR &= ~(uint32_t)(DMA_SxCR_CT);    
  }

  /* Write to DMAy Streamx M1AR */
  DMAy_Streamx->M1AR = Memory1BaseAddr;
}

/**
  * @brief  Enables or disables the double buffer mode for the selected DMA stream.
  * @note   This function can be called only when the DMA Stream is disabled.  
  * @param  DMAy_Streamx: where y can be 1 or 2 to select the DMA and x can be 0
  *          to 7 to select the DMA Stream.
  * @param  NewState: new state of the DMAy Streamx double buffer mode. 
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void DMA_DoubleBufferModeCmd(DMA_Stream_TypeDef* DMAy_Streamx, FunctionalState NewState)
{  
  /* Check the parameters */
  assert_param(IS_DMA_ALL_PERIPH(DMAy_Streamx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  /* Configure the Double Buffer mode */
  if (NewState != DISABLE)
  {
    /* Enable the Double buffer mode */
    DMAy_Streamx->CR |= (uint32_t)DMA_SxCR_DBM;
  }
  else
  {
    /* Disable the Double buffer mode */
    DMAy_Streamx->CR &= ~(uint32_t)DMA_SxCR_DBM;
  }
}

/**
  * @brief  Configures the Memory address for the next buffer transfer in double
  *         buffer mode (for dynamic use). This function can be called when the
  *         DMA Stream is enabled and when the transfer is ongoing.  
  * @param  DMAy_Streamx: where y can be 1 or 2 to select the DMA and x can be 0
  *          to 7 to select the DMA Stream.
  * @param  MemoryBaseAddr: The base address of the target memory buffer
  * @param  DMA_MemoryTarget: Next memory target to be used. 
  *         This parameter can be one of the following values:
  *            @arg DMA_Memory_0: To use the memory address 0
  *            @arg DMA_Memory_1: To use the memory address 1
  * 
  * @note    It is not allowed to modify the Base Address of a target Memory when
  *          this target is involved in the current transfer. ie. If the DMA Stream
  *          is currently transferring to/from Memory 1, then it not possible to
  *          modify Base address of Memory 1, but it is possible to modify Base
  *          address of Memory 0.
  *          To know which Memory is currently used, you can use the function
  *          DMA_GetCurrentMemoryTarget().             
  *  
  * @retval None
  */
void DMA_MemoryTargetConfig(DMA_Stream_TypeDef* DMAy_Streamx, uint32_t MemoryBaseAddr,
                           uint32_t DMA_MemoryTarget)
{
  /* Check the parameters */
  assert_param(IS_DMA_ALL_PERIPH(DMAy_Streamx));
  assert_param(IS_DMA_CURRENT_MEM(DMA_MemoryTarget));
    
  /* Check the Memory target to be configured */
  if (DMA_MemoryTarget != DMA_Memory_0)
  {
    /* Write to DMAy Streamx M1AR */
    DMAy_Streamx->M1AR = MemoryBaseAddr;    
  }  
  else
  {
    /* Write to DMAy Streamx M0AR */
    DMAy_Streamx->M0AR = MemoryBaseAddr;  
  }
}

/**
  * @brief  Returns the current memory target used by double buffer transfer.
  * @param  DMAy_Streamx: where y can be 1 or 2 to select the DMA and x can be 0
  *          to 7 to select the DMA Stream.
  * @retval The memory target number: 0 for Memory0 or 1 for Memory1. 
  */
uint32_t DMA_GetCurrentMemoryTarget(DMA_Stream_TypeDef* DMAy_Streamx)
{
  uint32_t tmp = 0;
  
  /* Check the parameters */
  assert_param(IS_DMA_ALL_PERIPH(DMAy_Streamx));

  /* Get the current memory target */
  if ((DMAy_Streamx->CR & DMA_SxCR_CT) != 0)
  {
    /* Current memory buffer used is Memory 1 */
    tmp = 1;
  }  
  else
  {
    /* Current memory buffer used is Memory 0 */
    tmp = 0;    
  }
  return tmp;
}
/**
  * @}
  */

/** @defgroup DMA_Group4 Interrupts and flags management functions
 *  @brief   Interrupts and flags management functions 
 *
@verbatim   
 ===============================================================================
                  Interrupts and flags management functions
 ===============================================================================  

  This subsection provides functions allowing to
   - Check the DMA enable status
   - Check the FIFO status 
   - Configure the DMA Interrupts sources and check or clear the flags or pending bits status.   
   
 1. DMA Enable status:
   After configuring the DMA Stream (DMA_Init() function) and enabling the stream,
   it is recommended to check (or wait until) the DMA Stream is effectively enabled.
   A Stream may remain disabled if a configuration parameter is wrong.
   After disabling a DMA Stream, it is also recommended to check (or wait until) the DMA
   Stream is effectively disabled. If a Stream is disabled while a data transfer is ongoing, 
   the current data will be transferred and the Stream will be effectively disabled only after
   this data transfer completion.
   To monitor this state it is possible to use the following function:
     - FunctionalState DMA_GetCmdStatus(DMA_Stream_TypeDef* DMAy_Streamx); 
 
 2. FIFO Status:
   It is possible to monitor the FIFO status when a transfer is ongoing using the following 
   function:
     - uint32_t DMA_GetFIFOStatus(DMA_Stream_TypeDef* DMAy_Streamx); 
 
 3. DMA Interrupts and Flags:
  The user should identify which mode will be used in his application to manage the
  DMA controller events: Polling mode or Interrupt mode. 
    
  Polling Mode
  =============
    Each DMA stream can be managed through 4 event Flags:
    (x : DMA Stream number )
       1. DMA_FLAG_FEIFx  : to indicate that a FIFO Mode Transfer Error event occurred.
       2. DMA_FLAG_DMEIFx : to indicate that a Direct Mode Transfer Error event occurred.
       3. DMA_FLAG_TEIFx  : to indicate that a Transfer Error event occurred.
       4. DMA_FLAG_HTIFx  : to indicate that a Half-Transfer Complete event occurred.
       5. DMA_FLAG_TCIFx  : to indicate that a Transfer Complete event occurred .       

   In this Mode it is advised to use the following functions:
      - FlagStatus DMA_GetFlagStatus(DMA_Stream_TypeDef* DMAy_Streamx, uint32_t DMA_FLAG);
      - void DMA_ClearFlag(DMA_Stream_TypeDef* DMAy_Streamx, uint32_t DMA_FLAG);

  Interrupt Mode
  ===============
    Each DMA Stream can be managed through 4 Interrupts:

    Interrupt Source
    ----------------
       1. DMA_IT_FEIFx  : specifies the interrupt source for the  FIFO Mode Transfer Error event.
       2. DMA_IT_DMEIFx : specifies the interrupt source for the Direct Mode Transfer Error event.
       3. DMA_IT_TEIFx  : specifies the interrupt source for the Transfer Error event.
       4. DMA_IT_HTIFx  : specifies the interrupt source for the Half-Transfer Complete event.
       5. DMA_IT_TCIFx  : specifies the interrupt source for the a Transfer Complete event. 
     
  In this Mode it is advised to use the following functions:
     - void DMA_ITConfig(DMA_Stream_TypeDef* DMAy_Streamx, uint32_t DMA_IT, FunctionalState NewState);
     - ITStatus DMA_GetITStatus(DMA_Stream_TypeDef* DMAy_Streamx, uint32_t DMA_IT);
     - void DMA_ClearITPendingBit(DMA_Stream_TypeDef* DMAy_Streamx, uint32_t DMA_IT);

@endverbatim
  * @{
  */

/**
  * @brief  Returns the status of EN bit for the specified DMAy Streamx.
  * @param  DMAy_Streamx: where y can be 1 or 2 to select the DMA and x can be 0
  *          to 7 to select the DMA Stream.
  *   
  * @note    After configuring the DMA Stream (DMA_Init() function) and enabling
  *          the stream, it is recommended to check (or wait until) the DMA Stream
  *          is effectively enabled. A Stream may remain disabled if a configuration
  *          parameter is wrong.
  *          After disabling a DMA Stream, it is also recommended to check (or wait 
  *          until) the DMA Stream is effectively disabled. If a Stream is disabled
  *          while a data transfer is ongoing, the current data will be transferred
  *          and the Stream will be effectively disabled only after the transfer
  *          of this single data is finished.  
  *      
  * @retval Current state of the DMAy Streamx (ENABLE or DISABLE).
  */
FunctionalState DMA_GetCmdStatus(DMA_Stream_TypeDef* DMAy_Streamx)
{
  FunctionalState state = DISABLE;

  /* Check the parameters */
  assert_param(IS_DMA_ALL_PERIPH(DMAy_Streamx));

  if ((DMAy_Streamx->CR & (uint32_t)DMA_SxCR_EN) != 0)
  {
    /* The selected DMAy Streamx EN bit is set (DMA is still transferring) */
    state = ENABLE;
  }
  else
  {
    /* The selected DMAy Streamx EN bit is cleared (DMA is disabled and 
        all transfers are complete) */
    state = DISABLE;
  }
  return state;
}

/**
  * @brief  Returns the current DMAy Streamx FIFO filled level.
  * @param  DMAy_Streamx: where y can be 1 or 2 to select the DMA and x can be 0 
  *         to 7 to select the DMA Stream.
  * @retval The FIFO filling state.
  *           - DMA_FIFOStatus_Less1QuarterFull: when FIFO is less than 1 quarter-full 
  *                                               and not empty.
  *           - DMA_FIFOStatus_1QuarterFull: if more than 1 quarter-full.
  *           - DMA_FIFOStatus_HalfFull: if more than 1 half-full.
  *           - DMA_FIFOStatus_3QuartersFull: if more than 3 quarters-full.
  *           - DMA_FIFOStatus_Empty: when FIFO is empty
  *           - DMA_FIFOStatus_Full: when FIFO is full
  */
uint32_t DMA_GetFIFOStatus(DMA_Stream_TypeDef* DMAy_Streamx)
{
  uint32_t tmpreg = 0;
 
  /* Check the parameters */
  assert_param(IS_DMA_ALL_PERIPH(DMAy_Streamx));
  
  /* Get the FIFO level bits */
  tmpreg = (uint32_t)((DMAy_Streamx->FCR & DMA_SxFCR_FS));
  
  return tmpreg;
}

/**
  * @brief  Checks whether the specified DMAy Streamx flag is set or not.
  * @param  DMAy_Streamx: where y can be 1 or 2 to select the DMA and x can be 0
  *          to 7 to select the DMA Stream.
  * @param  DMA_FLAG: specifies the flag to check.
  *          This parameter can be one of the following values:
  *            @arg DMA_FLAG_TCIFx:  Streamx transfer complete flag
  *            @arg DMA_FLAG_HTIFx:  Streamx half transfer complete flag
  *            @arg DMA_FLAG_TEIFx:  Streamx transfer error flag
  *            @arg DMA_FLAG_DMEIFx: Streamx direct mode error flag
  *            @arg DMA_FLAG_FEIFx:  Streamx FIFO error flag
  *         Where x can be 0 to 7 to select the DMA Stream.
  * @retval The new state of DMA_FLAG (SET or RESET).
  */
FlagStatus DMA_GetFlagStatus(DMA_Stream_TypeDef* DMAy_Streamx, uint32_t DMA_FLAG)
{
  FlagStatus bitstatus = RESET;
  DMA_TypeDef* DMAy;
  uint32_t tmpreg = 0;

  /* Check the parameters */
  assert_param(IS_DMA_ALL_PERIPH(DMAy_Streamx));
  assert_param(IS_DMA_GET_FLAG(DMA_FLAG));

  /* Determine the DMA to which belongs the stream */
  if (DMAy_Streamx < DMA2_Stream0)
  {
    /* DMAy_Streamx belongs to DMA1 */
    DMAy = DMA1; 
  } 
  else 
  {
    /* DMAy_Streamx belongs to DMA2 */
    DMAy = DMA2; 
  }

  /* Check if the flag is in HISR or LISR */
  if ((DMA_FLAG & HIGH_ISR_MASK) != (uint32_t)RESET)
  {
    /* Get DMAy HISR register value */
    tmpreg = DMAy->HISR;
  }
  else
  {
    /* Get DMAy LISR register value */
    tmpreg = DMAy->LISR;
  }   
 
  /* Mask the reserved bits */
  tmpreg &= (uint32_t)RESERVED_MASK;

  /* Check the status of the specified DMA flag */
  if ((tmpreg & DMA_FLAG) != (uint32_t)RESET)
  {
    /* DMA_FLAG is set */
    bitstatus = SET;
  }
  else
  {
    /* DMA_FLAG is reset */
    bitstatus = RESET;
  }

  /* Return the DMA_FLAG status */
  return  bitstatus;
}

/**
  * @brief  Clears the DMAy Streamx's pending flags.
  * @param  DMAy_Streamx: where y can be 1 or 2 to select the DMA and x can be 0
  *          to 7 to select the DMA Stream.
  * @param  DMA_FLAG: specifies the flag to clear.
  *          This parameter can be any combination of the following values:
  *            @arg DMA_FLAG_TCIFx:  Streamx transfer complete flag
  *            @arg DMA_FLAG_HTIFx:  Streamx half transfer complete flag
  *            @arg DMA_FLAG_TEIFx:  Streamx transfer error flag
  *            @arg DMA_FLAG_DMEIFx: Streamx direct mode error flag
  *            @arg DMA_FLAG_FEIFx:  Streamx FIFO error flag
  *         Where x can be 0 to 7 to select the DMA Stream.   
  * @retval None
  */
void DMA_ClearFlag(DMA_Stream_TypeDef* DMAy_Streamx, uint32_t DMA_FLAG)
{
  DMA_TypeDef* DMAy;

  /* Check the parameters */
  assert_param(IS_DMA_ALL_PERIPH(DMAy_Streamx));
  assert_param(IS_DMA_CLEAR_FLAG(DMA_FLAG));

  /* Determine the DMA to which belongs the stream */
  if (DMAy_Streamx < DMA2_Stream0)
  {
    /* DMAy_Streamx belongs to DMA1 */
    DMAy = DMA1; 
  } 
  else 
  {
    /* DMAy_Streamx belongs to DMA2 */
    DMAy = DMA2; 
  }

  /* Check if LIFCR or HIFCR register is targeted */
  if ((DMA_FLAG & HIGH_ISR_MASK) != (uint32_t)RESET)
  {
    /* Set DMAy HIFCR register clear flag bits */
    DMAy->HIFCR = (uint32_t)(DMA_FLAG & RESERVED_MASK);
  }
  else 
  {
    /* Set DMAy LIFCR register clear flag bits */
    DMAy->LIFCR = (uint32_t)(DMA_FLAG & RESERVED_MASK);
  }    
}

/**
  * @brief  Enables or disables the specified DMAy Streamx interrupts.
  * @param  DMAy_Streamx: where y can be 1 or 2 to select the DMA and x can be 0
  *          to 7 to select the DMA Stream.
  * @param DMA_IT: specifies the DMA interrupt sources to be enabled or disabled. 
  *          This parameter can be any combination of the following values:
  *            @arg DMA_IT_TC:  Transfer complete interrupt mask
  *            @arg DMA_IT_HT:  Half transfer complete interrupt mask
  *            @arg DMA_IT_TE:  Transfer error interrupt mask
  *            @arg DMA_IT_FE:  FIFO error interrupt mask
  * @param  NewState: new state of the specified DMA interrupts.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void DMA_ITConfig(DMA_Stream_TypeDef* DMAy_Streamx, uint32_t DMA_IT, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_DMA_ALL_PERIPH(DMAy_Streamx));
  assert_param(IS_DMA_CONFIG_IT(DMA_IT));
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  /* Check if the DMA_IT parameter contains a FIFO interrupt */
  if ((DMA_IT & DMA_IT_FE) != 0)
  {
    if (NewState != DISABLE)
    {
      /* Enable the selected DMA FIFO interrupts */
      DMAy_Streamx->FCR |= (uint32_t)DMA_IT_FE;
    }    
    else 
    {
      /* Disable the selected DMA FIFO interrupts */
      DMAy_Streamx->FCR &= ~(uint32_t)DMA_IT_FE;  
    }
  }

  /* Check if the DMA_IT parameter contains a Transfer interrupt */
  if (DMA_IT != DMA_IT_FE)
  {
    if (NewState != DISABLE)
    {
      /* Enable the selected DMA transfer interrupts */
      DMAy_Streamx->CR |= (uint32_t)(DMA_IT  & TRANSFER_IT_ENABLE_MASK);
    }
    else
    {
      /* Disable the selected DMA transfer interrupts */
      DMAy_Streamx->CR &= ~(uint32_t)(DMA_IT & TRANSFER_IT_ENABLE_MASK);
    }    
  }
}

/**
  * @brief  Checks whether the specified DMAy Streamx interrupt has occurred or not.
  * @param  DMAy_Streamx: where y can be 1 or 2 to select the DMA and x can be 0
  *          to 7 to select the DMA Stream.
  * @param  DMA_IT: specifies the DMA interrupt source to check.
  *          This parameter can be one of the following values:
  *            @arg DMA_IT_TCIFx:  Streamx transfer complete interrupt
  *            @arg DMA_IT_HTIFx:  Streamx half transfer complete interrupt
  *            @arg DMA_IT_TEIFx:  Streamx transfer error interrupt
  *            @arg DMA_IT_DMEIFx: Streamx direct mode error interrupt
  *            @arg DMA_IT_FEIFx:  Streamx FIFO error interrupt
  *         Where x can be 0 to 7 to select the DMA Stream.
  * @retval The new state of DMA_IT (SET or RESET).
  */
ITStatus DMA_GetITStatus(DMA_Stream_TypeDef* DMAy_Streamx, uint32_t DMA_IT)
{
  ITStatus bitstatus = RESET;
  DMA_TypeDef* DMAy;
  uint32_t tmpreg = 0, enablestatus = 0;

  /* Check the parameters */
  assert_param(IS_DMA_ALL_PERIPH(DMAy_Streamx));
  assert_param(IS_DMA_GET_IT(DMA_IT));
 
  /* Determine the DMA to which belongs the stream */
  if (DMAy_Streamx < DMA2_Stream0)
  {
    /* DMAy_Streamx belongs to DMA1 */
    DMAy = DMA1; 
  } 
  else 
  {
    /* DMAy_Streamx belongs to DMA2 */
    DMAy = DMA2; 
  }

  /* Check if the interrupt enable bit is in the CR or FCR register */
  if ((DMA_IT & TRANSFER_IT_MASK) != (uint32_t)RESET)
  {
    /* Get the interrupt enable position mask in CR register */
    tmpreg = (uint32_t)((DMA_IT >> 11) & TRANSFER_IT_ENABLE_MASK);   
    
    /* Check the enable bit in CR register */
    enablestatus = (uint32_t)(DMAy_Streamx->CR & tmpreg);
  }
  else 
  {
    /* Check the enable bit in FCR register */
    enablestatus = (uint32_t)(DMAy_Streamx->FCR & DMA_IT_FE); 
  }
 
  /* Check if the interrupt pending flag is in LISR or HISR */
  if ((DMA_IT & HIGH_ISR_MASK) != (uint32_t)RESET)
  {
    /* Get DMAy HISR register value */
    tmpreg = DMAy->HISR ;
  }
  else
  {
    /* Get DMAy LISR register value */
    tmpreg = DMAy->LISR ;
  } 

  /* mask all reserved bits */
  tmpreg &= (uint32_t)RESERVED_MASK;

  /* Check the status of the specified DMA interrupt */
  if (((tmpreg & DMA_IT) != (uint32_t)RESET) && (enablestatus != (uint32_t)RESET))
  {
    /* DMA_IT is set */
    bitstatus = SET;
  }
  else
  {
    /* DMA_IT is reset */
    bitstatus = RESET;
  }

  /* Return the DMA_IT status */
  return  bitstatus;
}

/**
  * @brief  Clears the DMAy Streamx's interrupt pending bits.
  * @param  DMAy_Streamx: where y can be 1 or 2 to select the DMA and x can be 0
  *          to 7 to select the DMA Stream.
  * @param  DMA_IT: specifies the DMA interrupt pending bit to clear.
  *          This parameter can be any combination of the following values:
  *            @arg DMA_IT_TCIFx:  Streamx transfer complete interrupt
  *            @arg DMA_IT_HTIFx:  Streamx half transfer complete interrupt
  *            @arg DMA_IT_TEIFx:  Streamx transfer error interrupt
  *            @arg DMA_IT_DMEIFx: Streamx direct mode error interrupt
  *            @arg DMA_IT_FEIFx:  Streamx FIFO error interrupt
  *         Where x can be 0 to 7 to select the DMA Stream.
  * @retval None
  */
void DMA_ClearITPendingBit(DMA_Stream_TypeDef* DMAy_Streamx, uint32_t DMA_IT)
{
  DMA_TypeDef* DMAy;

  /* Check the parameters */
  assert_param(IS_DMA_ALL_PERIPH(DMAy_Streamx));
  assert_param(IS_DMA_CLEAR_IT(DMA_IT));

  /* Determine the DMA to which belongs the stream */
  if (DMAy_Streamx < DMA2_Stream0)
  {
    /* DMAy_Streamx belongs to DMA1 */
    DMAy = DMA1; 
  } 
  else 
  {
    /* DMAy_Streamx belongs to DMA2 */
    DMAy = DMA2; 
  }

  /* Check if LIFCR or HIFCR register is targeted */
  if ((DMA_IT & HIGH_ISR_MASK) != (uint32_t)RESET)
  {
    /* Set DMAy HIFCR register clear interrupt bits */
    DMAy->HIFCR = (uint32_t)(DMA_IT & RESERVED_MASK);
  }
  else 
  {
    /* Set DMAy LIFCR register clear interrupt bits */
    DMAy->LIFCR = (uint32_t)(DMA_IT & RESERVED_MASK);
  }   
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
